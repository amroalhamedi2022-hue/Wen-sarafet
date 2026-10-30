# Batch 17 — Safe Google Drive backup publication (2026-10-30)

**Base:** Batch 16 PR #19, `7cf600d`. **PR:** #20 (Draft). This is an engineering acceptance note, not proof of a real-device release.

## Previous data-loss path
`DriveBackupManager.sync` opened `Wen-Sarafet-latest.wsb` with `"wt"` before the new backup had finished. A write, provider or encryption failure could truncate the only working remote backup.

## New publication protocol
1. Create a unique `Wen-Sarafet-pending-<UUID>.wsb` in the selected SAF/Drive folder.
2. Stream an encrypted snapshot into that pending document; close the writer.
3. **Read the pending document back from the provider**, fully decrypt AES-GCM, validate ZIP CRC and required `database.sqlite` / `settings.properties` entries, and verify the SQLite file header. Reject incomplete or unreadable uploads before modifying the latest.
4. Rename the existing `Wen-Sarafet-latest.wsb` to a unique `Wen-Sarafet-previous-<UUID>.wsb`.
5. Rename the verified pending document to `Wen-Sarafet-latest.wsb`; verify the name and that the document can be located.
6. If promotion fails, try restoring the previous document to the latest name. **Never delete verified pending or previous recovery files on ambiguous provider errors.** Incomplete, unverified pending files may be removed.

SAF/Google Drive does **not** guarantee atomic rename, instant visibility, or durable remote sync. There can be a brief interval with no `latest` filename, but the previous backup remains under its recovery filename. If the provider fails during rollback, recover using the `previous` document rather than assuming the `latest` filename exists.

### Retention and user impact
Previous recovery documents are intentionally not automatically deleted. Repeated syncs can accumulate backups and use Drive storage; a separate, carefully reviewed retention policy is required before enabling automatic pruning. Do not delete these files blindly. A manually selected recovery document can be used with the existing encrypted restore flow after confirming its password and authenticity.

### Automated checks
- `DriveBackupPublicationTest`: successful first/second publication; failed write/read-back; pre/post rename exceptions; rollback; pending recovery.
- `BackupArchiveVerifierTest`: real encrypted ZIP read-back; missing entries; invalid database header; wrong password; truncated/tampered GCM; temp cleanup.
- `drive_backup_publication_contract.py`: source wiring and regression guards.
- `scripts/test_core.sh`, Android debug compilation/Lint and unsigned Release CI are required; do not claim device success from these alone.

### NOT TESTED
Google Drive DocumentsProvider behavior under poor connectivity, remote upload lag, process death, duplicate filenames, low storage, and actual device restore. No signed production APK was published for this batch. No change to security-sensitive protected files, DB schema, package name or signing keys.
