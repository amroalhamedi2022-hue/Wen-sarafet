package app.wensarafet.tracker;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * Reserve distinct restore staging files and rollback destinations.
 * A wall-clock millisecond is not a unique identifier: concurrent restores
 * used to share one plaintext staging file and one rollback filename.
 */
final class BackupRestoreStaging {
    private BackupRestoreStaging() {}

    static File createTemporaryDatabase(File cacheDirectory) throws IOException {
        // Atomically creates an exclusive file; the restore writer can truncate
        // this reserved file without touching any other in-progress restore.
        return File.createTempFile("wen-restore-", ".db", cacheDirectory);
    }

    static File newRollbackDestination(File currentDatabase) {
        File directory = currentDatabase.getParentFile();
        if (directory == null) throw new IllegalArgumentException("Database has no parent directory");
        // Do not create, truncate or delete a previous recovery file merely
        // to reserve its name. The caller must refuse an existing destination.
        return new File(directory, "wen_sarafet.pre_restore." + UUID.randomUUID() + ".db");
    }
}
