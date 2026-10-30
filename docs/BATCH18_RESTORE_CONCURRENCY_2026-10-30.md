# Batch 18 — collision-free backup/restore staging (2026-10-30)

**Base:** Batch 17 PR #20, commit `6541f209`. **Status:** draft; no release or merge.

## Confirmed regression path
Before this batch, `BackupFileManager.restore` used `restore-<System.currentTimeMillis()>.db` as its plaintext staging path and `wen_sarafet.pre_restore.<System.currentTimeMillis()>.db` for rollback. Two restores started within the same millisecond could target the **same** file, so one operation could overwrite or delete the other's staged database. Concurrent manual/Drive backup snapshots could also run during database replacement.

## Fix
- `BackupRestoreStaging.createTemporaryDatabase` atomically reserves a unique temporary file with `File.createTempFile`.
- `BackupRestoreStaging.newRollbackDestination` chooses a random UUID-based path without truncating or deleting existing recovery copies. Existing-path rejection remains.
- `BackupFileManager.create` and `restore` are synchronized static methods to serialize snapshot creation and restore in this app process; they share the same class monitor.
- The restore ZIP parser rejects duplicate `database.sqlite` entries and missing database entries rather than silently accepting ambiguous archives.
- Added a pure-JVM concurrency regression test with 300 concurrent filename allocations and preservation of a pre-existing recovery file. Source/SQLite preflight contracts now guard the implementation.

## Verification
Required CI: `scripts/test_core.sh`, protected-file hash checks, source safety guard, Android debug compile/Lint and unsigned release candidate. These do **not** prove Android real-device restore behavior. Check GitHub Actions on the final HEAD.

## Limitations
- In-process serialization does not protect against a separate app process or arbitrary third-party file manipulation. Android provider behavior, interruption/reboot and SQLite migration must still be verified on a real device using disposable test data.
- No changes to signing keys, integrity, PIN, licensing, backup encryption, package identity or DB schema.
- Do not distribute an APK from this branch as an installed-app update until signature and migration tests are completed.
