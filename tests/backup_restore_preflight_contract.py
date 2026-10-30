#!/usr/bin/env python3
"""Backup restore preflight contract and real SQLite corruption/schema fixtures.

The SQL tests execute on SQLite; Android SQLiteDatabase behavior still needs a device test.
"""
from pathlib import Path
import sqlite3
import tempfile

ROOT = Path(__file__).resolve().parents[1]
SOURCE = (ROOT / "app/src/main/java/app/wensarafet/tracker/BackupFileManager.java").read_text()
HELPER = (ROOT / "app/src/main/java/app/wensarafet/tracker/FinanceDbHelper.java").read_text()
RESTORE = SOURCE.split("public static synchronized void restore(", 1)[1].split(
    "private static Properties collectPreferences", 1
)[0]

assert "static final int DB_VERSION = 13" in HELPER
assert "validateSqlite(temp)" in RESTORE
assert RESTORE.index("validateSqlite(temp)") < RESTORE.index("target.renameTo(old)")
assert RESTORE.index("checkpointLiveDatabase(target)") < RESTORE.index("target.renameTo(old)")
assert "BackupRestoreStaging.newRollbackDestination(target)" in RESTORE
assert "old.delete();" not in RESTORE.split("target.renameTo(old)", 1)[0]
assert "PRAGMA wal_checkpoint(TRUNCATE)" in SOURCE
assert "checkpoint.getInt(0) != 0" in SOURCE
assert "PRAGMA integrity_check" in SOURCE
assert "PRAGMA foreign_key_check" in SOURCE
assert "FinanceDbHelper.DB_VERSION" in SOURCE
assert "SQLiteDatabase.OPEN_READONLY" in SOURCE
assert "name IN ('accounts','categories','transactions','transaction_splits')" in SOURCE
assert "try (FinanceDbHelper check = new FinanceDbHelper(context))" in RESTORE
# A timestamp-named staging file could collide and let one restore overwrite
# another's plaintext or rollback copy; a backup could race the replacement.
assert "public static synchronized void create(" in SOURCE
assert "public static synchronized void restore(" in SOURCE
assert "BackupRestoreStaging.createTemporaryDatabase(context.getCacheDir())" in RESTORE
assert "System.currentTimeMillis()" not in RESTORE
assert "if (foundDatabase) throw new IllegalArgumentException" in RESTORE
assert "if (!foundDatabase)" in RESTORE
# ZIP central directory can terminate parsing before the AES-GCM tag is read.
# Complete authentication must happen before SQLite preflight/replacement.
assert "BackupArchiveLimits.readSettings(inputZip)" in RESTORE
assert "BackupArchiveLimits.MAX_DATABASE_BYTES" in RESTORE
assert "BackupArchiveLimits.MAX_ENTRIES" in RESTORE
assert "BackupArchiveLimits.MAX_EXTRA_BYTES" in RESTORE
assert "BackupRestoreAuthentication.finish(decrypted)" in RESTORE
assert RESTORE.index("BackupRestoreAuthentication.finish(decrypted)") < RESTORE.index("validateSqlite(temp)")
# Regression: a fixed snapshot filename allowed simultaneous backups to overwrite
# each other, and malformed/absent settings could silently wipe preferences.
assert 'File.createTempFile("wen-backup-", ".db", context.getCacheDir())' in SOURCE
assert '"backup-snapshot.db"' not in SOURCE
assert "BackupSettingsCodec.decode(properties)" in RESTORE
assert RESTORE.index("BackupSettingsCodec.decode(properties)") < RESTORE.index("target.renameTo(old)")
assert "if (foundSettings)" in RESTORE
assert "if (decodedSettings == null) return;" in SOURCE
assert "if (!editor.commit())" in SOURCE
assert "failure.addSuppressed(rollbackError)" in SOURCE
rollback = RESTORE.split("} catch (Exception error) {", 1)[-1]
assert rollback.index('new File(target.getPath() + "-wal").delete()') < rollback.index("old.renameTo(target)")
assert rollback.index('new File(target.getPath() + "-shm").delete()') < rollback.index("old.renameTo(target)")


def make_database(path, version=13, missing_splits=False, orphan=False):
    with sqlite3.connect(path) as db:
        db.executescript("""
            CREATE TABLE accounts(id INTEGER PRIMARY KEY, balance_minor INTEGER NOT NULL);
            CREATE TABLE categories(id INTEGER PRIMARY KEY);
            CREATE TABLE transactions(id INTEGER PRIMARY KEY, account_id INTEGER NOT NULL,
                FOREIGN KEY(account_id) REFERENCES accounts(id));
        """)
        if not missing_splits:
            db.execute("CREATE TABLE transaction_splits(id INTEGER PRIMARY KEY)")
        db.execute("INSERT INTO accounts VALUES(1, 12345)")
        db.execute("INSERT INTO categories VALUES(1)")
        db.execute("INSERT INTO transactions VALUES(1, ?)", (999 if orphan else 1,))
        db.execute(f"PRAGMA user_version = {version}")


def preflight_sql(path):
    # Equivalent SQLite checks to the Android preflight (no replacement performed).
    with sqlite3.connect(f"file:{path}?mode=ro", uri=True) as db:
        version = db.execute("PRAGMA user_version").fetchone()[0]
        if not 1 <= version <= 13:
            return False
        if db.execute("PRAGMA integrity_check").fetchall() != [("ok",)]:
            return False
        if db.execute("PRAGMA foreign_key_check").fetchone() is not None:
            return False
        tables = db.execute(
            "SELECT COUNT(*) FROM sqlite_master WHERE type='table' "
            "AND name IN ('accounts','categories','transactions','transaction_splits')"
        ).fetchone()[0]
        return tables == 4


with tempfile.TemporaryDirectory() as directory:
    folder = Path(directory)
    cases = {
        "valid-v13": (13, False, False, True),
        "valid-old-v8": (8, False, False, True),
        "future-v14": (14, False, False, False),
        "missing-splits": (13, True, False, False),
        "broken-financial-reference": (13, False, True, False),
        "unversioned": (0, False, False, False),
    }
    for name, (version, missing, orphan, expected) in cases.items():
        path = folder / (name + ".sqlite")
        make_database(path, version, missing, orphan)
        assert preflight_sql(path) == expected, name
        if expected:
            with sqlite3.connect(path) as db:
                assert db.execute("SELECT balance_minor FROM accounts").fetchone()[0] == 12345
    damaged = folder / "damaged.sqlite"
    damaged.write_bytes(b"SQLite format 3\x00" + b"garbage")
    try:
        preflight_sql(damaged)
    except sqlite3.DatabaseError:
        pass
    else:
        raise AssertionError("A truncated SQLite database must not pass")

print("BackupRestorePreflight: SQLite version, tables, FK, corruption, and source-order guards passed (device NOT TESTED).")
