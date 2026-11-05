# Wen Sarafet — B06 source hardening (2026-10-25)

## Critical defect addressed

In `FinanceDbHelper.deleteTransaction`, the cursor query returned exactly **five** columns (`type,amount_minor,account_id,destination_account_id,affects_balance`) but the method accessed indexes **0 through 17**. Any delete could throw `CursorIndexOutOfBoundsException` before undo logic. The projection now explicitly includes all **20** fields used by deletion checks, including `base_affects_balance`, category, timestamp, source, debt linkage and all FX quote metadata. Snapshots are forbidden for special FX/debt metadata or linked splits/capture/debt movements.

## Category Undo consistency

- `undoLatestSimpleCategoryChange` now runs its eligibility query, reclassification and marking the audit `undone=1` inside one outer SQLite transaction.
- The history-screen eligibility query and the actual action **share one guard**, checking manual source, no pending review, no splits, debt/capture links or FX metadata, and both category types still match the transaction.
- A stale audit ID, missing category or a failed audit-mark update rolls back the transaction instead of leaving a changed balance and a live Undo action.
- Android supports nested SQLite transactions with outermost rollback control, per official SQLiteDatabase documentation; no third-party source code copied.

## Evidence / automated tests

- `tests/finance_delete_cursor_and_category_undo_contract.py` executes the **exact projection and guard queries extracted from Java** using in-memory SQLite, checks every accessed cursor index, rejects source='CAPTURED'/FX/debt/capture/split, and exercises audit failure rollback.
- `bash scripts/test_core.sh` runs the whole previous source suite plus this test.
- `sha256sum -c tests/protected_security_sha256.txt` verifies 7 protected files unchanged.
- No DB schema change or new migration; DB stays at v12.

## Explicit limits

- No Android APK build, instrumentation tests or in-place upgrade to 1.8.0 has been run.
- This is a **source-level** correction; cannot claim that actual device deletion UX is verified.
- Automatic Undo of deleted transfers, debts and split transactions intentionally remains disabled.
- No secrets, signing keystore, private license generator or personal financial data were added to the public repository.
- Branch to stack on prior batch05; do not merge `main` before release gates.
