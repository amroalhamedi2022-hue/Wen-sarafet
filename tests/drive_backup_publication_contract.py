#!/usr/bin/env python3
"""Source-level guard: Drive sync must not truncate the last known-good WSB.

The JVM failure-injection suite exercises the publication state machine;
DocumentsProvider/Drive consistency still requires an Android device test.
"""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
src = root / "app/src/main/java/app/wensarafet/tracker"
manager = (src / "DriveBackupManager.java").read_text()
publication = (src / "DriveBackupPublication.java").read_text()
verifier = (src / "BackupArchiveVerifier.java").read_text()

assert "DriveBackupPublication.publish(" in manager
assert "BackupArchiveVerifier.verify(input, secret, context.getCacheDir())" in manager
assert "DocumentsContract.renameDocument" in manager
assert "DocumentsContract.deleteDocument" in manager
assert 'openOutputStream(document, "wt")' in manager
assert 'openOutputStream(target, "wt")' not in manager
assert "RUNNING.compareAndSet(false, true)" in manager
assert "أكثر من ملف بنفس اسم النسخة" in manager
assert "store.write(pending)" in publication
assert "store.verify(pending)" in publication
assert publication.index("store.verify(pending)") < publication.index("store.rename(current, previousName)")
assert publication.index("store.rename(current, previousName)") < publication.index("store.rename(pending, LATEST_NAME)")
assert "if (store.find(LATEST_NAME) == null)" in publication
assert "failure.addSuppressed(rollbackFailure)" in publication
assert "if (!verified)" in publication
assert "BackupCrypto.decryptingStream" in verifier
assert "BackupArchiveLimits.copy(decrypted, output, BackupArchiveLimits.MAX_ARCHIVE_BYTES)" in verifier
assert "BackupArchiveLimits.MAX_SETTINGS_BYTES" in verifier
assert "BackupArchiveLimits.MAX_ENTRIES" in verifier
assert "new ZipFile(plain)" in verifier
assert "ZipInputStream" not in verifier.split("import java.util.zip.ZipFile;", 1)[1].split("final class BackupArchiveVerifier", 1)[0]
assert "crc.getValue() != declaredCrc" in verifier
assert "actualSize != declaredSize" in verifier
assert "if (!database || !settings)" in verifier

print("DriveBackupPublicationContract passed: no in-place truncation, authenticated stage, recovery guards (device NOT TESTED).")
