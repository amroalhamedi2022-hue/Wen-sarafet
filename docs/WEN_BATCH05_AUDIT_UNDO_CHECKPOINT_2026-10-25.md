# Wen Sarafet — Batch 05: safe undo of simple manual transaction deletion

Date: 2026-10-25. Status: SOURCE INTEGRATION / TESTS ONLY, NOT ANDROID RELEASE.

## User scope
C2 audit history under Settings and Undo. Older code logged deletions but omitted restorable state. This batch adds **narrowly scoped** deletion undo, not full financial/transfer/debt undo.

## Implemented changes
- SQLite schema v11 → v12: `deleted_transaction_snapshots` stores original transaction data and **exact balance effect reversed at deletion**. `onCreate` and `onUpgrade` both create table. Older audit deletion entries intentionally remain non-undoable (no snapshot can be reconstructed).
- A qualifying DELETE in `FinanceDbHelper.deleteTransaction` snapshots under the same SQLite transaction after the DELETE trigger logs the event. Failure rolls back deletion and balance.
- `undoDeletedSimpleTransaction(auditId)` atomically validates snapshot + record absence, original account/currency, category type, then restores original transaction ID and balance effect, sets `restored_at`, marks audit undone. A new ADD audit event is expected from the original audit trigger.
- Restoring a transaction and deleting it again creates a **new audit snapshot** without colliding with the previous restored one.
- Audit history UI shows the Undo button for qualifying deletions only, and preserves previous category-change Undo.
- Pure-Java `SimpleUndoRules` enforces eligibility and overflow-safe math.
- Eligibility narrowly requires MANUAL EXPENSE/INCOME, positive amount, simple single category, no split, no debt, no capture reference, no transfer, no special FX/debt metadata, no pending review. Other deletions remain in the history but cannot be automatically undone.
- No changes to seven protected licensing, PIN, biometrics, Keystore, integrity and backup crypto Java files.

## Verification
- `bash scripts/test_core.sh` passed, including JVM `SimpleUndoRulesTest` and Python/real sqlite3 `simple_delete_undo_v12.py` alongside older core, debt, FX, v10 and v11 migration tests.
- `sha256sum -c tests/protected_security_sha256.txt` passed 7/7.
- SQLite tests check actual v12 DDL, linked audit ID, restore/re-delete, FK constraints, and failing-restore rollback; they simulate Java execution rather than executing Android SQLiteOpenHelper.
- Android Gradle build, instrumentation, signed upgrade from production 1.8.0, and actual UI tests **NOT RUN**.

## Known limitations and next gates
- Full rollback for ordinary transaction edits, multi-splits, debt settlements, transfers/FX, category removal, and older deletion logs **NOT IMPLEMENTED**.
- Before any production APK, test upgrade of a **redacted** real DB 9→10→11→12, validate balances and `PRAGMA foreign_key_check` on Android; test deleted-capture exclusions, double undo, category archived or removed, same-record ID collision, and lock/license integrity.
- The recovered 1.8.0 source remains distinct from the lost original Git history. No owner secrets may enter this public repo.

References: Android SQLiteOpenHelper upgrade transaction semantics, https://developer.android.com/reference/android/database/sqlite/SQLiteOpenHelper ; SQLite foreign key rules https://www.sqlite.org/foreignkeys.html . No code copied from these documents.
