# Batch 19 — bounded encrypted backup imports and Drive read-back (2026-10-30)

**Base:** Batch 18 PR #21, commit `854e273f`. **Status:** draft; no release, signing or merge.

## Reproduced issue

The restore path previously streamed `database.sqlite` to disk without a size ceiling, parsed `settings.properties` directly through unbounded `Properties.load`, and implicitly consumed unknown ZIP entries. Drive read-back verification also decrypted the entire archive to a plaintext cache file without a size ceiling. A malformed imported archive or pathological compressed entry could fill storage or exhaust heap **before** the live-database replacement safety checks ran.

## Change

- Add pure-Java `BackupArchiveLimits` with a 1 GiB database limit, 1.25 GiB encrypted ZIP plaintext limit, 2 MiB settings/optional-entry limit and maximum 16 entries. Limits are intentionally generous but finite; oversized archives fail explicitly, never truncate.
- Apply bounds to manual/Drive backup creation so the app does not generate a database/settings archive it cannot restore.
- Apply bounds to restore before database replacement and to Drive read-back, including decompressed ZIP entries (not just compressed sizes). A bounded decrypted stream also covers ZIP directory/trailing bytes, not merely named entries.
- Preserve optional legacy ZIP entries up to a bounded size and preserve all existing SQLite integrity, migration, GCM, and rollback checks.
- Add JVM regression tests for exact-boundary behavior and compressed oversized settings/unknown entries; strengthen the restore source contract.

## Limitations

No device restore or Drive provider test was run. An archive beyond these safety limits is rejected with an error; future support for unusually large archives should use a separately reviewed streaming import. Existing APK 1.8.1 does not include this batch. No changes to protected security files, schema version, license, signing or app identity.
