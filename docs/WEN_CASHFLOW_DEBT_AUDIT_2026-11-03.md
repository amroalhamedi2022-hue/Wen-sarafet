# Wen Sarafet — cashflow/debt accounting audit (Draft, 2026-11-03)

## Owner-reported failure
Paying for a brother correctly deducted account funds on purchase, but converting the expense to a loan/debt erroneously refunded the account. Expense analytics must NOT count the loan; the account balance MUST remain debited. An actual receivable should be recorded in the debt ledger and only a real repayment should increase cash.

## Root cause confirmed in original production development source
- `reclassifyTransactionAsDebt` reversed the old expense, then set `affects_balance=0` for new `DEBT` rows and did not re-post the debit.
- `addDebtTransaction` treated opening debt as reference-only, unlike physical money movements.
- `exclude_from_expense` incorrectly influenced the account posting amount via `expenseBalanceImpact`, `SplitDebtRules` and manual edit calculations.
- Existing tests explicitly asserted the incorrect behaviour, so green Actions did not prove proper accounting.

## Proposed Draft-only implementation
- Every recorded physical money movement posts once when its explicit `base_affects_balance` is true, including loans, repayments and expense categories excluded from spending analytics.
- Expense / category / monthly-budget analytics continue to exclude loans and categories marked excluded. Debt movements remain in their ledger and are not ordinary expenses.
- SQLite v14 persists `balance_impact_minor` per transaction. A v13-to-v14 migration derives original, **historical engine** postings for pre-existing rows, including legacy zero-debit loans and excluded expenses. The migration does not UPDATE accounts, debt ledger, amounts, dates, categories, or checkpoints. Historical provider-captured transactions whose original source-balance delta cannot be reconstructed are explicitly marked with UNKNOWN posting impact (NULL), and any attempt to reverse or recategorise them is safely rejected without changing account balances; post-v14 captures record actual deltas.
- Edits, recategorisation, debt edits, deletion, simple deletion undo, captures and FX now use the durable original posting where relevant, instead of re-deriving it from the current category. Imported provider balances preserve their actual applied delta.
- All changes remain in a new isolated functional Draft branch based on PR #26, not main, and do not touch the independent Liquid Glass UI.

## Sample expected behaviour (ILS)
1. Account has 1,000. Spend 100 -> account 900; ordinary spending +100.
2. Convert that purchase to `DEBT / RECEIVABLE` for brother -> account **still 900**; ordinary spending goes back to baseline; debt receivable +100.
3. Brother repays 40 into the account -> account 940; open receivable 60; ordinary spending stays unchanged.
4. Delete an eligible original transaction only with confirmation -> reverse the exact amount originally posted; deletion/undo must remain atomic.
5. Mixed expense 20 + excluded item 30 + loan 50 (total 100) -> account -100, spending +20, debt receivable +50.

## Historical-account remediation safety
No automatic historic account-balance correction is permitted here. Legacy pre-v14 transactions may have led to artificially high displayed balances. If users manually corrected old balances, auto-subtraction could double-charge them. The initial migration only records what the previous code actually posted. A read-only discrepancy review and separate, explicitly confirmed correction must happen on a backup copy before real-device adjustments.

## Validation gates
- Run `bash scripts/test_core.sh`, `python3 tests/financial_safe_zone_guard.py`, Android compile/lint, and backup-restore preflight on GitHub Actions.
- Run v13->v14 regression on a **fictional SQLite fixture**, verify unchanged old account values, debt movements, categories, raw transaction data, and foreign keys.
- Before signing or merging: backup current tablet financial data, test in-place owner-signed upgrade with the matching certificate and correct versionCode, confirm actual bank balances and historical transactions, confirm debt partial payments, edit/delete/undo, capture imports, FX, category checkpoints/monthly budget history, encrypted local/Drive backup and restore.
- `main` remains documentation-only. Do not install debug or unsigned release candidates over production.
- Source/CI success cannot prove the user's existing private finance data is intact, nor prove no unknown historic discrepancy exists.

**Status:** patch candidate under test; do not describe as installed, released or fully validated.

## Additional safety hardening
- An explicit manual debt-to-category exit is permitted only for an intact manual non-FX, non-split debt row with ledger provenance; outstanding repayments on a debt opening must be handled first. The generic editor still rejects arbitrary debt/FX changes.
- New original-package 1.8.6 release in GitHub CI remains **UNSIGNED**; required original owner signing keystore is not available in repository or CI. Signed update and real-device validation remain blockers to shipping.

## Captured message atomicity repair
- Ordinary imported provider expenses/income check PENDING inside their SQLite write transaction and require a single-row compare-and-swap acknowledgement; failed acknowledgement rolls back the new transaction and account effect.
- Own-account transfer imports now commit both account changes, a transaction row, and inbox status together; invalid account selection does not mark a message imported. Duplicate imports are rejected.
- The read-only safety test uses a fictional SQLite fixture. It cannot establish that historical live captures were free of duplicates; reconciliation is required.
