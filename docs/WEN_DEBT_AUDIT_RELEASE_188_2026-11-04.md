# Wen Sarafet 1.8.8 — debt/category and audit-history repair
Date: 2026-11-04. Owner explicitly requested this targeted finance/UI repair.

## Reproduced user-facing defects (screenshots 2026-11-03)
- Debt transaction -> ordinary expense category showed generic toast "العملية مرتبطة بتحويل أو دين؛ استخدم تعديلها المخصص".
- Audit cards used `fieldParams()` (fixed 54dp height), clipping their own details, events and actions. The audit presentation did not interpret DEBT_* event payloads.

## Repairs
- New `reclassifyDebtTransactionToCategory` dedicated atomic path removes the debt ledger allocation, changes only category and special-debt identity and recalculates affected debt summaries. NEVER reverses or re-applies the original cash posting; does not edit source account, amount, currency, existing posted impact, timestamps or capture metadata. Permits compatible manual/captured source, **same financial type only**, and rejects split/FX/corrupt ledger records. If repayment depends on opening, refuse with actionable message. This protects historical debt transactions with uncertain imported/old cash postings.
- Debt editing UI routes directly to same-direction category picker. The previous generic guard remains for other transfer/FX/debt mutation.
- Separate clearly marked ledger-only repayments from real-account repayments. Real repayment picks an active account matching the debt currency, calls the atomic `addDebtPaymentToExactDebt` method targeting the clicked debt ID (not another older debt); receivable INCOME adds cash, payable EXPENSE debits cash. Ledger-only closing explicitly warns it never changes any account.
- Audit events now render WRAP_CONTENT cards, searchable history with category filter, full pagination (40 items per page), event ID vs linked record ID, before/after values with names of accounts and debt people, and optional "show original" link. Delete/undo remains restricted to existing eligible safe actions.
- Manual `setAccountBalance` now writes a before/after ACCOUNT_ADJUST entry in the same SQLite transaction. No log is created for an unchanged balance.
- Database migration v14->v15 **refreshes audit triggers only**, preserving account/transaction/debt/backup/checkpoint rows. New audit triggers also capture note updates, debt note, category name/emoji/status and original transaction context. Old audit entries are never retroactively fabricated; legacy sparse entries still display whatever can be verified.
- Production version 1.8.8 (versionCode 24) > signed 1.8.7 (versionCode 23). Signing uses private owner key outside public GitHub.

## Known limitations
- Existing historical account balances previously impacted by legacy debt/excluded classification remain unchanged automatically; manual reconciliation against provider statements is still required. Do not delete or repeatedly recategorise original real-money records.
- If a debt opening has a repayment that would become orphaned, the change is refused. Do not delete real repayments to bypass this without reviewing history first.
- Historic audit data that was not recorded cannot be reconstructed; deleted debt records may no longer have a human-readable person.
- Physical owner-device 1.8.7->1.8.8 upgrade, activation, backups, balances, ledger, and widget must be verified with a backup before declaring a production-safe release. CI Java/SQLite contracts and compile alone are insufficient.

## Acceptance matrix (fictional test account)
1. Signed install over existing app, no data cleared; verify backups, device PIN, activation, existing accounts/debts/categories and monthly checkpoints.
2. Start with ILS 1000, record 100 expense -> 900. Reclassify as RECEIVABLE debt -> still 900, analytics decrease 100, debt increase 100.
3. Debt -> ordinary EXPENSE category -> account stays 900, receivable decreases 100, analytics increase 100.
4. Add separate 100 receivable opening. Record **real** 40 repayment into chosen ILS account -> balance +40, debt -40, expense analytics unchanged. A separate **ledger-only** 10 repayment must change debt -10 but NOT account.
5. Attempt to detach debt opening after linked repayments; must refuse without any partial account or history change.
6. Audit history card heights must show titles, dates, before/after details; filtered 40-event pages traverse old entries. Tap card opens detailed history; safe undo exposed only for eligible manual simple transactions.
7. Manually adjust an isolated test account balance; see one before/after ACCOUNT_ADJUST audit entry, not repeated on no-op save.
8. Back up and restore on isolated device/test data, ensure no old balance duplicated, no category checkpoint loss, no deleted real money records.

No theme/Liquid Glass animation source changes were made.

## Exact debt payment protection
The prior general ledger allocation chose the oldest debt of the same person/currency, not necessarily the debt card clicked. The real-account repayment control now posts the payment only to the requested `debt.id`, guards insufficient funds and overpayment, and commits the account change, financial transaction and debt movement together. The original general-purpose historical allocation method remains unchanged for other workflows.
